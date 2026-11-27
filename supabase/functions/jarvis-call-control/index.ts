import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
const cors = {"Access-Control-Allow-Origin":"*","Access-Control-Allow-Headers":"authorization, x-client-info, apikey, content-type","Access-Control-Allow-Methods":"POST, OPTIONS","Content-Type":"application/json"};
const out=(status:number,data:Record<string,unknown>)=>new Response(JSON.stringify(data),{status,headers:cors});
const str=(v:unknown,n:number)=>typeof v==="string"?v.trim().slice(0,n):"";
Deno.serve(async(req:Request)=>{
 if(req.method==="OPTIONS")return new Response("ok",{headers:cors});
 if(req.method!=="POST")return out(405,{error:"POST required"});
 const authorization=req.headers.get("Authorization")??"";
 if(!authorization.startsWith("Bearer "))return out(401,{error:"Sign in first."});
 const url=Deno.env.get("SUPABASE_URL"),key=Deno.env.get("SUPABASE_ANON_KEY");
 if(!url||!key)return out(503,{error:"Function environment incomplete."});
 const db=createClient(url,key,{global:{headers:{Authorization:authorization}},auth:{persistSession:false,autoRefreshToken:false}});
 const {data:user,error:userError}=await db.auth.getUser();
 if(userError||!user.user)return out(401,{error:"Session invalid or expired."});
 let b:Record<string,unknown>;try{b=await req.json()}catch{return out(400,{error:"Valid JSON required."})}
 const owner=user.user.id,action=str(b.action,24);
 if(action==="register_device"){
  const device=str(b.device_id,128),token=str(b.fcm_token,4096);
  if(!device||!token)return out(400,{error:"device_id and fcm_token required."});
  const {error}=await db.from("jarvis_devices").upsert({owner_id:owner,device_id:device,fcm_token:token,platform:"android",app_version:str(b.app_version,40),enabled:true,updated_at:new Date().toISOString()},{onConflict:"owner_id,device_id"});
  return error?out(400,{error:error.message}):out(200,{ok:true,action});
 }
 if(action==="disable_device"){
  const device=str(b.device_id,128);if(!device)return out(400,{error:"device_id required."});
  const {error}=await db.from("jarvis_devices").update({enabled:false,updated_at:new Date().toISOString()}).eq("owner_id",owner).eq("device_id",device);
  return error?out(400,{error:error.message}):out(200,{ok:true,action});
 }
 if(action==="create_interaction"){
  const device=str(b.device_id,128),task=str(b.task_id,128),question=str(b.question,2000);
  if(!device||!task||!question)return out(400,{error:"device_id, task_id and question required."});
  const {data:registered}=await db.from("jarvis_devices").select("id").eq("owner_id",owner).eq("device_id",device).eq("enabled",true).maybeSingle();
  if(!registered)return out(409,{error:"No enabled JARVIS device registered."});
  const {data:call,error:ce}=await db.from("jarvis_calls").insert({owner_id:owner,device_id:device,task_id:task,direction:"agent_to_device",state:"ringing"}).select("id,state,created_at").single();
  if(ce||!call)return out(400,{error:ce?.message??"Call creation failed."});
  const checkpoint=b.checkpoint&&typeof b.checkpoint==="object"?b.checkpoint:{};
  const {data:interaction,error:ie}=await db.from("jarvis_task_interactions").insert({owner_id:owner,task_id:task,call_id:call.id,question,checkpoint,state:"calling"}).select("id,task_id,question,state,created_at").single();
  if(ie||!interaction){await db.from("jarvis_calls").delete().eq("id",call.id);return out(400,{error:ie?.message??"Checkpoint creation failed."})}
  await db.from("jarvis_call_events").insert({owner_id:owner,call_id:call.id,event_type:"state",payload:{state:"ringing",interaction_id:interaction.id}});
  return out(201,{ok:true,call,interaction});
 }
 const callId=str(b.call_id,64);if(!callId)return out(400,{error:"call_id required."});
 if(["answer","decline","hangup"].includes(action)){
  const state=action==="answer"?"active":action==="decline"?"declined":"ended",now=new Date().toISOString();
  const patch:Record<string,unknown>={state};if(action==="answer")patch.answered_at=now;else patch.ended_at=now;
  const {data:call,error}=await db.from("jarvis_calls").update(patch).eq("id",callId).select("id,state,task_id").maybeSingle();
  if(error||!call)return out(404,{error:"Call not found or not owned by this account."});
  await db.from("jarvis_task_interactions").update({state:action==="answer"?"answered":"cancelled",...(action==="answer"?{answered_at:now}:{})}).eq("call_id",callId);
  await db.from("jarvis_call_events").insert({owner_id:owner,call_id:callId,event_type:"state",payload:{state}});
  return out(200,{ok:true,call});
 }
 if(action==="submit_answer"){
  const transcript=str(b.transcript,8000);if(!transcript)return out(400,{error:"transcript required."});
  const {data:interaction,error}=await db.from("jarvis_task_interactions").update({answer_transcript:transcript,state:"resuming",answered_at:new Date().toISOString()}).eq("call_id",callId).select("id,task_id,checkpoint,answer_transcript,state").maybeSingle();
  if(error||!interaction)return out(404,{error:"Task interaction not found."});
  await db.from("jarvis_call_events").insert({owner_id:owner,call_id:callId,event_type:"transcript",payload:{transcript}});
  return out(200,{ok:true,interaction});
 }
 if(action==="mark_resumed"){
  const {data:interaction,error}=await db.from("jarvis_task_interactions").update({state:"resumed",resumed_at:new Date().toISOString()}).eq("call_id",callId).select("id,task_id,state,resumed_at").maybeSingle();
  if(error||!interaction)return out(404,{error:"Task interaction not found."});
  await db.from("jarvis_calls").update({state:"ended",ended_at:new Date().toISOString()}).eq("id",callId);
  return out(200,{ok:true,interaction});
 }
 return out(400,{error:"Unsupported action."});
});