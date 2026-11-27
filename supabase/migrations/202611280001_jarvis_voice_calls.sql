-- JARVIS in-app voice calling and task-interruption state.
-- Apply only after a Supabase project is connected. RLS is owner-scoped.
create extension if not exists pgcrypto;
create table if not exists public.jarvis_calls (id uuid primary key default gen_random_uuid(), owner_id uuid not null references auth.users(id) on delete cascade, device_id text not null, task_id text, state text not null default 'ringing' check (state in ('ringing','connecting','active','ended','missed','declined','failed')), direction text not null check (direction in ('agent_to_device','device_to_agent')), created_at timestamptz not null default now(), answered_at timestamptz, ended_at timestamptz, end_reason text, metadata jsonb not null default '{}'::jsonb);
create table if not exists public.jarvis_task_interactions (id uuid primary key default gen_random_uuid(), owner_id uuid not null references auth.users(id) on delete cascade, task_id text not null, call_id uuid references public.jarvis_calls(id) on delete set null, question text not null, checkpoint jsonb not null default '{}'::jsonb, answer_transcript text, state text not null default 'waiting' check (state in ('waiting','calling','answered','resuming','resumed','cancelled','expired')), created_at timestamptz not null default now(), answered_at timestamptz, resumed_at timestamptz);
create table if not exists public.jarvis_call_events (id bigint generated always as identity primary key, owner_id uuid not null references auth.users(id) on delete cascade, call_id uuid not null references public.jarvis_calls(id) on delete cascade, event_type text not null check (event_type in ('offer','answer','ice_candidate','transcript','state','hangup')), payload jsonb not null default '{}'::jsonb, created_at timestamptz not null default now());
create index if not exists jarvis_calls_owner_created_idx on public.jarvis_calls(owner_id, created_at desc);
create index if not exists jarvis_interactions_task_state_idx on public.jarvis_task_interactions(owner_id, task_id, state);
create index if not exists jarvis_call_events_call_created_idx on public.jarvis_call_events(call_id, created_at);
alter table public.jarvis_calls enable row level security;
alter table public.jarvis_task_interactions enable row level security;
alter table public.jarvis_call_events enable row level security;
drop policy if exists owner_manages_own_calls on public.jarvis_calls;
create policy owner_manages_own_calls on public.jarvis_calls for all to authenticated using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
drop policy if exists owner_manages_own_task_interactions on public.jarvis_task_interactions;
create policy owner_manages_own_task_interactions on public.jarvis_task_interactions for all to authenticated using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
drop policy if exists owner_manages_own_call_events on public.jarvis_call_events;
create policy owner_manages_own_call_events on public.jarvis_call_events for all to authenticated using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
do $$ begin alter publication supabase_realtime add table public.jarvis_calls; exception when duplicate_object then null; when undefined_object then raise notice 'Enable Realtime publication for jarvis_calls'; end $$;
do $$ begin alter publication supabase_realtime add table public.jarvis_task_interactions; exception when duplicate_object then null; when undefined_object then raise notice 'Enable Realtime publication for jarvis_task_interactions'; end $$;
do $$ begin alter publication supabase_realtime add table public.jarvis_call_events; exception when duplicate_object then null; when undefined_object then raise notice 'Enable Realtime publication for jarvis_call_events'; end $$;
