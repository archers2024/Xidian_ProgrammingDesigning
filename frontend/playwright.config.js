import {defineConfig} from '@playwright/test'
import fs from 'node:fs'
const session=JSON.parse(fs.readFileSync('../backend/target/test-session.json','utf8'))
if(!/^campus_test_\d+$/.test(session.TEST_SCHEMA))throw Error('E2E requires isolated test schema')
export default defineConfig({outputDir:'../backend/target/e2e-'+Date.now(),testDir:'tests/e2e',workers:1,timeout:30000,use:{channel:process.platform==='win32'?'msedge':undefined,baseURL:'http://127.0.0.1:4173',trace:'retain-on-failure',screenshot:'only-on-failure'},webServer:{command:'npm run preview -- --host 127.0.0.1 --port 4173',url:'http://127.0.0.1:4173',reuseExistingServer:false,env:{CAMPUS_API_TARGET:session.TEST_BASE}}})
