package com.campus.activity.module.system.storage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
/** Adapter for a private HTTPS object gateway supporting PUT/GET/DELETE at /key. */
@Component @ConditionalOnProperty(name="campus.file.storage",havingValue="http")
public class HttpObjectStorage implements FileStorage {
    private final URI endpoint;private final String token;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public HttpObjectStorage(@Value("${campus.file.object-endpoint}")String endpoint,@Value("${campus.file.object-token}")String token){this.endpoint=URI.create(endpoint.endsWith("/")?endpoint:endpoint+"/");if(!"https".equals(this.endpoint.getScheme()))throw new IllegalArgumentException("Object gateway requires HTTPS");this.token=token;}
    private HttpRequest.Builder request(String key){if(!key.matches("[A-Za-z0-9_./-]+")||key.contains("..")||key.startsWith("/"))throw new IllegalArgumentException("Invalid key");return HttpRequest.newBuilder(endpoint.resolve(key)).timeout(Duration.ofSeconds(45)).header("Authorization","Bearer "+token);}
    private HttpResponse<InputStream> send(HttpRequest r)throws IOException{try{var response=client.send(r,HttpResponse.BodyHandlers.ofInputStream());if(response.statusCode()/100!=2){response.body().close();throw new IOException("Object gateway status "+response.statusCode());}return response;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}}
    public void put(String key,InputStream input,long size)throws IOException{try(var body=send(request(key).PUT(HttpRequest.BodyPublishers.ofInputStream(()->input)).build()).body()) {}}
    public InputStream open(String key)throws IOException{return send(request(key).GET().build()).body();}
    public void delete(String key)throws IOException{try{var response=client.send(request(key).DELETE().build(),HttpResponse.BodyHandlers.discarding());if(response.statusCode()/100!=2&&response.statusCode()!=404)throw new IOException("Object gateway status "+response.statusCode());}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}}
}
