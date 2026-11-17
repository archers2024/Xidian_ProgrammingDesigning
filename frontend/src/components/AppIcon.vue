<template><svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path v-for="(path,index) in (paths[name] || paths.activity)" :key="index" :d="path"/></svg></template>
<script setup>
defineProps({name:{type:String,default:'activity'},size:{type:[Number,String],default:20}})
const paths={
 workspace:['M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z'],
 activity:['M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z','M7 3v4 M17 3v4 M3 11h18 M7 15h3 M14 15h3'],
 chart:['M4 3v17h17 M9 15V9 M14 15V5 M19 15v-4'],
 review:['M9 4H6a2 2 0 0 0-2 2v14h16V6a2 2 0 0 0-2-2h-3','M9 2h6v5H9z M8 14l3 3 5-6'],
 team:['M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M16 4a4 4 0 0 1 0 8 M22 21v-2a4 4 0 0 0-3-3.87','M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z'],
 organization:['M8 3h8v6H8z M3 16h6v5H3z M15 16h6v5h-6z M12 9v4 M6 16v-3h12v3'],
 audit:['M14 2H5v20h14V7z M14 2v5h5 M8 11h8 M8 15h8 M8 19h4'],
 plus:['M12 5v14 M5 12h14'],arrow:['M5 12h14 M13 6l6 6-6 6'],
 clock:['M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z M12 6v6l4 2'],
 location:['M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z','M15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z'],
 people:['M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2','M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z M17 8h5 M19.5 5.5v5'],
 check:['M20 6 9 17l-5-5']
}
</script>
