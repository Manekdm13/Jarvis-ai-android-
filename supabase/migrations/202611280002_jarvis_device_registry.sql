create table if not exists public.jarvis_devices (
 id uuid primary key default gen_random_uuid(),
 owner_id uuid not null references auth.users(id) on delete cascade,
 device_id text not null,
 fcm_token text not null,
 platform text not null default 'android' check (platform in ('android')),
 app_version text,
 enabled boolean not null default true,
 updated_at timestamptz not null default now(),
 unique(owner_id, device_id)
);
create index if not exists jarvis_devices_owner_enabled_idx on public.jarvis_devices(owner_id, enabled);
alter table public.jarvis_devices enable row level security;
drop policy if exists owner_manages_own_devices on public.jarvis_devices;
create policy owner_manages_own_devices on public.jarvis_devices for all to authenticated using ((select auth.uid()) = owner_id) with check ((select auth.uid()) = owner_id);
