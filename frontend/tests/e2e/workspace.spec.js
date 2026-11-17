import {test,expect} from '@playwright/test'
import fs from 'node:fs'
import {spawnSync} from 'node:child_process'
const config=JSON.parse(fs.readFileSync('../backend/target/test-session.json','utf8'))
async function login(page){await page.goto('/login');await page.getByPlaceholder('请输入账号',{exact:true}).fill(config.TEST_ADMIN_USERNAME);await page.getByPlaceholder('请输入密码',{exact:true}).fill(config.TEST_ADMIN_PASSWORD);await page.getByRole('button',{name:'登录',exact:true}).click();await expect(page).toHaveURL(/dashboard/)}
test('production bundle, HttpOnly session, navigation and reload',async({page,context},testInfo)=>{
 const errors=[];page.on('pageerror',e=>errors.push(e.message));await login(page)
 await expect(page.getByRole('heading',{name:/好/})).toBeVisible()
 await page.screenshot({path:testInfo.outputPath('desktop-dashboard.png'),fullPage:true})
 expect(await page.evaluate(()=>localStorage.getItem('token'))).toBeNull()
 const cookie=(await context.cookies()).find(c=>c.name==='CAMPUS_SESSION');expect(cookie?.httpOnly).toBe(true)
 await page.reload();await expect(page).toHaveURL(/dashboard/)
 await page.getByRole('menuitem',{name:'活动管理',exact:true}).click();await expect(page.getByRole('heading',{name:'活动管理',exact:true})).toBeVisible()
 await page.getByRole('menuitem',{name:'数据统计',exact:true}).click();await expect(page.getByRole('heading',{name:'数据统计',exact:true})).toBeVisible()
 await page.goto('/not-an-existing-page');await expect(page.getByRole('heading',{name:'页面不存在'})).toBeVisible();expect(errors).toEqual([])
})
test('mobile width navigation and form error focus',async({page},testInfo)=>{
 await page.setViewportSize({width:390,height:844});await login(page)
 await page.getByRole('button',{name:'新建活动',exact:true}).click()
 await page.getByRole('button',{name:'保存活动',exact:true}).click()
 await expect(page.getByText('请输入活动名称',{exact:true})).toBeVisible()
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+2)).toBe(true)
 await page.screenshot({path:testInfo.outputPath('mobile-create.png'),fullPage:true})
})
test('mobile activity check-in page displays the real activity title',async({page,request})=>{
 const login=await request.post(config.TEST_BASE+'/auth/login',{data:{username:config.TEST_ADMIN_USERNAME,password:config.TEST_ADMIN_PASSWORD}});const token=(await login.json()).data.token
 const time=h=>new Date(Date.now()+(8+h)*3600000).toISOString().slice(0,19)
 const result=await request.post(config.TEST_BASE+'/admin/activity',{headers:{Authorization:'Bearer '+token},data:{title:'E2E现场活动',typeCode:'LECTURE',location:'测试场地',quota:10,signupDeadline:time(1),startTime:time(2),endTime:time(3)}});const id=(await result.json()).data.activityId
 try{await request.post(config.TEST_BASE+`/admin/activity/${id}/publish`,{headers:{Authorization:'Bearer '+token}});await page.goto(`/m/checkin/${id}`);await expect(page.getByRole('heading',{name:'E2E现场活动'})).toBeVisible()}finally{await request.delete(config.TEST_BASE+`/admin/activity/${id}`,{headers:{Authorization:'Bearer '+token}})}
})

test('organization approval, role boundary, mobile signup and check-in workflow',async({browser,request},testInfo)=>{
 test.setTimeout(90000)
 const tag='UI_'+Date.now(),password='UiAcceptance12345',headers={};let id,orgId,accountId,adminContext,rootContext,studentContext
 const loginResponse=await request.post(config.TEST_BASE+'/auth/login',{data:{username:config.TEST_ADMIN_USERNAME,password:config.TEST_ADMIN_PASSWORD}});headers.Authorization='Bearer '+(await loginResponse.json()).data.token
 const org=await request.post(config.TEST_BASE+'/super/organizations',{headers,data:{code:tag,name:'界面验收组织'}});orgId=(await org.json()).data.orgId
 await request.post(config.TEST_BASE+'/super/admin/accounts',{headers,data:{username:tag,realName:'界面验收员',roleCode:'ADMIN',orgId,passwordHash:password}})
 const accounts=await request.get(config.TEST_BASE+'/super/admin/accounts/page?keyword='+tag,{headers});accountId=(await accounts.json()).data.records[0].adminId
 try{
  adminContext=await browser.newContext();const admin=await adminContext.newPage()
  await admin.goto('http://127.0.0.1:4173/login');await admin.getByPlaceholder('请输入账号',{exact:true}).fill(tag);await admin.getByPlaceholder('请输入密码',{exact:true}).fill(password);await admin.getByRole('button',{name:'登录',exact:true}).click();await expect(admin).toHaveURL(/dashboard/)
  await admin.goto('http://127.0.0.1:4173/platform/organizations');await expect(admin.getByRole('heading',{name:'没有访问权限'})).toBeVisible()
  await admin.goto('http://127.0.0.1:4173/activity/create')
  await admin.getByLabel('活动名称',{exact:false}).fill(tag+'校园活动');await admin.getByLabel('活动地点',{exact:false}).fill('学生中心')
  const types=await request.get(config.TEST_BASE+'/dict/activity-type');const lecture=(await types.json()).data.find(item=>item.typeCode==='LECTURE').typeName
  await admin.locator('.el-select').filter({has:admin.getByLabel('活动类型',{exact:false})}).click();await admin.getByRole('option',{name:lecture,exact:true}).click()
  const time=h=>new Date(Date.now()+(8+h)*3600000).toISOString().slice(0,19).replace('T',' ')
  await admin.locator('.el-range-input[placeholder="开始时间"]').fill(time(2));await admin.locator('.el-range-input[placeholder="结束时间"]').fill(time(3));await admin.locator('.el-range-input[placeholder="结束时间"]').press('Tab')
  await admin.getByPlaceholder('必须早于活动开始时间').fill(time(1));await admin.getByPlaceholder('必须早于活动开始时间').press('Tab')
  await admin.getByRole('button',{name:'保存活动',exact:true}).click();await expect(admin).toHaveURL(/\/activity$/)
  await admin.getByRole('button',{name:tag+'校园活动',exact:true}).click();await expect(admin.getByRole('button',{name:'提交审核',exact:true})).toBeVisible();id=Number(new URL(admin.url()).pathname.split('/').at(-1))
  await admin.getByRole('button',{name:'提交审核',exact:true}).click();await expect(admin.getByText(/审批状态：待审批/)).toBeVisible()
  rootContext=await browser.newContext();const root=await rootContext.newPage();await login(root);await root.goto('http://127.0.0.1:4173/platform/reviews')
  const row=root.getByRole('row').filter({hasText:tag+'校园活动'});await row.getByRole('button',{name:'申请内容',exact:true}).click();await expect(root.getByRole('dialog',{name:'提交时的活动内容'})).toBeVisible();await root.screenshot({path:testInfo.outputPath('review-snapshot.png'),fullPage:true});await root.getByRole('dialog').getByRole('button',{name:/close|关闭/i}).first().click()
  await row.getByRole('button',{name:'审批',exact:true}).click();await root.getByRole('dialog').getByRole('button',{name:'确认处理',exact:true}).click();await expect(row).toHaveCount(0)
  await admin.reload();await admin.getByRole('button',{name:'发布活动',exact:true}).click();await expect(admin.getByRole('button',{name:'发布活动',exact:true})).toHaveCount(0)
  const detail=await request.get(config.TEST_BASE+'/admin/activity/'+id,{headers});const guid=(await detail.json()).data.activity.signupGuid
  studentContext=await browser.newContext({viewport:{width:390,height:844}});const student=await studentContext.newPage();await student.goto('http://127.0.0.1:4173/m/promo/'+guid);await expect(student.getByRole('img',{name:'活动报名二维码',exact:true})).toBeVisible();expect(await student.getByRole('img',{name:'活动报名二维码',exact:true}).evaluate(image=>image.naturalWidth)).toBeGreaterThan(0);await student.screenshot({path:testInfo.outputPath('mobile-promo-qr.png'),fullPage:true});await student.getByRole('button',{name:'立即报名',exact:true}).click()
  await student.getByPlaceholder('请输入 2 至 20 字姓名').fill('界面验收学生');await student.getByPlaceholder('请输入手机号').fill('13987654321');await student.getByLabel('学院').selectOption({index:1});await student.getByRole('button',{name:'确认报名',exact:true}).click();await expect(student.getByRole('heading',{name:'报名成功',exact:true})).toBeVisible()
  await admin.getByRole('button',{name:'编辑活动',exact:true}).click();await admin.getByLabel('活动名称',{exact:false}).fill(tag+'发布后修订');await admin.getByRole('button',{name:'保存活动',exact:true}).click();await expect(admin).toHaveURL(/\/activity$/)
  await expect(admin.getByRole('columnheader',{name:'发布时间',exact:true})).toBeVisible();await admin.getByRole('button',{name:tag+'发布后修订',exact:true}).click();await expect(admin.getByText('1 / 100',{exact:true})).toBeVisible()
  const revised=await request.get(config.TEST_BASE+'/admin/activity/'+id,{headers});expect((await revised.json()).data.activity.signupGuid).toBe(guid)
  await admin.screenshot({path:testInfo.outputPath('published-edited-with-registration.png'),fullPage:true})
  const update=spawnSync(config.MYSQL_BIN,['--host=127.0.0.1','--user='+config.TEST_DB_USER,'--database='+config.TEST_SCHEMA,'-e',`UPDATE t_activity SET status='ONGOING',start_time=DATE_SUB(NOW(),INTERVAL 1 HOUR),signup_deadline=DATE_SUB(NOW(),INTERVAL 2 HOUR),end_time=DATE_ADD(NOW(),INTERVAL 1 HOUR),checkin_enabled=1 WHERE activity_id=${id}`],{env:{...process.env,MYSQL_PWD:config.TEST_DB_PASSWORD},encoding:'utf8'});expect(update.status).toBe(0)
  await student.goto('http://127.0.0.1:4173/m/checkin/'+id);await student.getByLabel('报名手机号').fill('13987654321');await student.getByRole('button',{name:'确认签到',exact:true}).click();await expect(student.getByText('签到成功',{exact:true})).toBeVisible();await student.screenshot({path:testInfo.outputPath('mobile-checkin.png'),fullPage:true})
  await admin.goto('http://127.0.0.1:4173/activity');const updatedRow=admin.getByRole('row').filter({hasText:tag+'发布后修订'});await updatedRow.getByRole('button',{name:'删除活动',exact:true}).click();await admin.getByRole('dialog',{name:'删除活动',exact:true}).getByRole('button',{name:'确定',exact:true}).click();await expect(updatedRow).toHaveCount(0);const removed=await request.get(config.TEST_BASE+'/admin/activity/'+id,{headers});expect(removed.status()).toBe(404)
 }finally{if(id)await request.delete(config.TEST_BASE+'/admin/activity/'+id,{headers}).catch(()=>{});if(accountId)await request.put(config.TEST_BASE+'/super/admin/accounts/'+accountId+'/status',{headers,data:{status:0}}).catch(()=>{});if(orgId)await request.put(config.TEST_BASE+'/super/organizations/'+orgId+'/status',{headers,data:{status:0}}).catch(()=>{});for(const context of [adminContext,rootContext,studentContext].filter(Boolean))await context.close()}
})
