package com.campus.events;

import com.campus.events.checkin.ChallengeTokens;
import com.campus.events.checkin.CheckinService;
import com.campus.events.registration.RegistrationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CampusFlowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private CheckinService checkinService;
    @Autowired
    private ChallengeTokens challengeTokens;

    @Test
    void anonymousAdminApiIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/activities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void loginThenCreatePublishAndRegister() throws Exception {
        Client client = login();
        String body = """
                {
                  "name": "接口联调活动",
                  "typeId": "type-lecture",
                  "description": "用于验证创建、发布和报名",
                  "location": "教学楼 101",
                  "startAt": "%s",
                  "endAt": "%s",
                  "registrationDeadline": "%s",
                  "capacity": 2,
                  "liveUrl": "https://live.example.edu.cn/demo"
                }
                """.formatted(iso(48), iso(50), iso(36));
        MvcResult created = client.postJson("/api/v1/admin/activities", body);
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();

        client.postJson("/api/v1/admin/activities/" + id + "/publish", "");
        mockMvc.perform(get("/api/v1/public/activities/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayStatus").value("REGISTERING"))
                .andExpect(jsonPath("$.data.canRegister").value(true));

        String register = """
                {"name":"测试同学","phone":"13800138111","collegeId":"col-cs"}
                """;
        postPublic("/api/v1/public/activities/" + id + "/registrations", register)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityName").value("接口联调活动"));
        postPublic("/api/v1/public/activities/" + id + "/registrations", register)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_DUPLICATE"));
    }

    @Test
    void onlyOneRegistrationSucceedsWhenOneSeatRemains() throws Exception {
        Client client = login();
        String id = createPublished(client, 1);
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String phone = "1390000" + String.format("%04d", 2000 + i);
            results.add(pool.submit(() -> {
                start.await();
                try {
                    registrationService.register(id, new com.campus.events.registration.RegistrationDtos.CreateRequest(
                            "并发同学", phone, "col-cs"));
                    return true;
                } catch (RuntimeException exception) {
                    return false;
                }
            }));
        }
        start.countDown();
        int success = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                success++;
            }
        }
        pool.shutdown();
        assertEquals(1, success);
    }

    @Test
    void dynamicTokenRejectsTamperingAndExpiry() throws Exception {
        Client client = login();
        String activityId = createPublished(client, 5);
        String open = """
                {"mode":"DYNAMIC","windowStart":"%s","windowEnd":"%s"}
                """.formatted(iso(-1), iso(5));
        MvcResult opened = client.postJson("/api/v1/admin/activities/" + activityId + "/checkin-sessions", open);
        String sessionId = objectMapper.readTree(opened.getResponse().getContentAsString())
                .path("data").path("id").asText();
        MvcResult challenge = client.getJson("/api/v1/admin/checkin-sessions/" + sessionId + "/challenge");
        String token = objectMapper.readTree(challenge.getResponse().getContentAsString()).path("data").path("token").asText();

        String register = """
                {"name":"签到同学","phone":"13800138222","collegeId":"col-math"}
                """;
        postPublic("/api/v1/public/activities/" + activityId + "/registrations", register)
                .andExpect(status().isOk());

        String submit = """
                {"sessionId":"%s","phone":"13800138222","token":"%s"}
                """.formatted(sessionId, token);
        postPublic("/api/v1/public/checkins", submit)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadyCheckedIn").value(false));
        postPublic("/api/v1/public/checkins", submit)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadyCheckedIn").value(true));

        String tampered = token + "x";
        postPublic("/api/v1/public/checkins", """
                {"sessionId":"%s","phone":"13800138222","token":"%s"}
                """.formatted(sessionId, tampered))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHECKIN_TOKEN_INVALID"));

        String expired = challengeTokens.issue(sessionId, activityId, 1, Instant.now().minusSeconds(120));
        postPublic("/api/v1/public/checkins", """
                {"sessionId":"%s","phone":"13800138222","token":"%s"}
                """.formatted(sessionId, expired))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHECKIN_TOKEN_EXPIRED"));
        assertTrue(checkinService.summary(activityId).checkedInCount() >= 1);
    }

    private String createPublished(Client client, int capacity) throws Exception {
        String body = """
                {
                  "name": "名额活动",
                  "typeId": "type-club",
                  "description": "名额测试",
                  "location": "活动室",
                  "startAt": "%s",
                  "endAt": "%s",
                  "registrationDeadline": "%s",
                  "capacity": %d,
                  "liveUrl": null
                }
                """.formatted(iso(24), iso(26), iso(20), capacity);
        MvcResult created = client.postJson("/api/v1/admin/activities", body);
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();
        client.postJson("/api/v1/admin/activities/" + id + "/publish", "");
        return id;
    }

    private Client login() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode csrf = objectMapper.readTree(csrfResult.getResponse().getContentAsString()).path("data");
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .session(session)
                        .header(csrf.path("headerName").asText(), csrf.path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andReturn();
        MockHttpSession authenticated = (MockHttpSession) login.getRequest().getSession(false);
        MvcResult fresh = mockMvc.perform(get("/api/v1/auth/csrf").session(authenticated)).andReturn();
        JsonNode freshToken = objectMapper.readTree(fresh.getResponse().getContentAsString()).path("data");
        return new Client(authenticated, freshToken.path("headerName").asText(), freshToken.path("token").asText());
    }

    private org.springframework.test.web.servlet.ResultActions postPublic(String url, String body) throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf")).andReturn();
        JsonNode csrf = objectMapper.readTree(csrfResult.getResponse().getContentAsString()).path("data");
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        return mockMvc.perform(post(url)
                .session(session)
                .header(csrf.path("headerName").asText(), csrf.path("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String iso(int hoursFromNow) {
        return OffsetDateTime.now(ZoneOffset.ofHours(8)).plusHours(hoursFromNow).withNano(0).toString();
    }

    private final class Client {
        private final MockHttpSession session;
        private final String header;
        private final String token;

        private Client(MockHttpSession session, String header, String token) {
            this.session = session;
            this.header = header;
            this.token = token;
        }

        private MvcResult postJson(String url, String body) throws Exception {
            return mockMvc.perform(post(url)
                            .session(session)
                            .header(header, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body == null || body.isBlank() ? "{}" : body))
                    .andExpect(status().isOk())
                    .andReturn();
        }

        private MvcResult getJson(String url) throws Exception {
            return mockMvc.perform(get(url).session(session))
                    .andExpect(status().isOk())
                    .andReturn();
        }
    }
}
