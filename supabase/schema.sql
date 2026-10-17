-- =====================================================================
--  SmartPreOrder  -  Supabase schema (run once in: SQL Editor -> New query)
-- =====================================================================

create extension if not exists "pgcrypto";

-- ---------- TABLES ----------------------------------------------------
create table public.profiles (
  id          uuid primary key references auth.users(id) on delete cascade,
  full_name   text not null default '',
  phone       text,
  role        text not null default 'customer' check (role in ('customer','restaurant','admin')),
  is_blocked  boolean not null default false,
  created_at  timestamptz not null default now()
);

create table public.restaurants (
  id                          uuid primary key default gen_random_uuid(),
  owner_id                    uuid not null references public.profiles(id) on delete cascade,
  name                        text not null,
  description                 text,
  address                     text,
  phone                       text,
  email                       text,
  category_id                 uuid,
  image_url                   text,
  cover_image_url             text,
  city                        text,
  district                    text,
  postal_code                 text,
  latitude                    double precision,
  longitude                   double precision,
  business_registration_no    text,
  min_order_amount            numeric(10,2) not null default 0,
  estimated_prep_time_minutes integer,
  delivery_radius_km          numeric(6,2),
  approval_status             text not null default 'pending' check (approval_status in ('pending','under_review','approved','rejected')),
  rejection_reason            text,
  submitted_at                timestamptz,
  approved_at                 timestamptz,
  is_open                     boolean not null default true,    -- owner toggles (accepting orders)
  is_active                   boolean not null default true,    -- admin toggles (listed in the app)
  created_at                  timestamptz not null default now()
);
create unique index restaurants_one_per_owner on public.restaurants(owner_id);

create table public.categories (
  id    uuid primary key default gen_random_uuid(),
  name  text not null unique
);

create table public.menu_items (
  id            uuid primary key default gen_random_uuid(),
  restaurant_id uuid not null references public.restaurants(id) on delete cascade,
  category_id   uuid references public.categories(id) on delete set null,
  name          text not null,
  description   text,
  price         numeric(10,2) not null check (price >= 0),
  image_url     text,
  is_available  boolean not null default true,
  created_at    timestamptz not null default now()
);
create index on public.menu_items(restaurant_id);

create table public.orders (
  id                uuid primary key default gen_random_uuid(),
  customer_id       uuid not null references public.profiles(id),
  restaurant_id     uuid not null references public.restaurants(id),
  order_type        text not null check (order_type in ('dine_in','pickup')),
  status            text not null default 'pending'
                    check (status in ('pending','accepted','preparing','ready','completed','rejected','cancelled')),
  total             numeric(10,2) not null default 0,
  note              text,
  estimated_minutes int,
  reject_reason     text,
  table_no          text,
  customer_name     text,
  customer_phone    text,
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now()
);
create index on public.orders(customer_id);
create index on public.orders(restaurant_id);

create table public.order_items (
  id           uuid primary key default gen_random_uuid(),
  order_id     uuid not null references public.orders(id) on delete cascade,
  menu_item_id uuid references public.menu_items(id) on delete set null,
  item_name    text not null,
  unit_price   numeric(10,2) not null,
  quantity     int not null check (quantity > 0)
);
create index on public.order_items(order_id);

create table public.reviews (
  id            uuid primary key default gen_random_uuid(),
  order_id      uuid not null unique references public.orders(id) on delete cascade,
  restaurant_id uuid not null references public.restaurants(id) on delete cascade,
  customer_id   uuid not null references public.profiles(id),
  customer_name text,
  rating        int not null check (rating between 1 and 5),
  comment       text,
  created_at    timestamptz not null default now()
);

create table public.reports (
  id            uuid primary key default gen_random_uuid(),
  reporter_id   uuid not null references public.profiles(id) on delete cascade,
  restaurant_id uuid references public.restaurants(id) on delete cascade,
  reason        text not null,
  status        text not null default 'open' check (status in ('open','resolved')),
  created_at    timestamptz not null default now()
);

create table public.promotions (
  id            uuid primary key default gen_random_uuid(),
  title         text,
  discount_text text,
  subtext       text,
  image_url     text,
  start_color   text default '#900C3F',
  end_color     text default '#C70039',
  is_active     boolean not null default true,
  created_at    timestamptz not null default now()
);

create table public.sales_reports (
  id            uuid primary key default gen_random_uuid(),
  restaurant_id uuid not null references public.restaurants(id) on delete cascade,
  period_type   text not null check (period_type in ('daily', 'weekly', 'monthly', 'all')),
  period_label  text not null,
  total_sales   numeric(10,2) not null default 0.00,
  total_orders  int not null default 0,
  file_url      text,
  created_at    timestamptz not null default now()
);

-- average rating per restaurant
create view public.restaurant_stats with (security_invoker = true) as
  select restaurant_id,
         round(avg(rating)::numeric, 1) as avg_rating,
         count(*)::int                  as review_count
  from public.reviews
  group by restaurant_id;

-- ---------- HELPER FUNCTIONS -----------------------------------------
create or replace function public.is_admin() returns boolean
language sql stable security definer set search_path = public as $$
  select coalesce((select role = 'admin' from public.profiles where id = auth.uid()), false)
$$;

create or replace function public.my_role() returns text
language sql stable security definer set search_path = public as $$
  select role from public.profiles where id = auth.uid()
$$;

create or replace function public.owns_restaurant(rid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.restaurants where id = rid and owner_id = auth.uid())
$$;

create or replace function public.can_see_order(oid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from public.orders o
    where o.id = oid
      and (o.customer_id = auth.uid() or public.owns_restaurant(o.restaurant_id) or public.is_admin())
  )
$$;

-- ---------- TRIGGERS --------------------------------------------------
-- 1) create a profile row whenever someone signs up (role: customer or restaurant only)
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, full_name, phone, role)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'full_name', ''),
    new.raw_user_meta_data->>'phone',
    case
      when new.raw_user_meta_data->>'role' in ('admin', 'restaurant', 'customer')
      then new.raw_user_meta_data->>'role'
      else 'customer'
    end
  );
  return new;
end $$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- 2) normal users can't change their own role / blocked flag
create or replace function public.protect_profile_columns() returns trigger
language plpgsql as $$
begin
  if auth.uid() is not null and not public.is_admin()
     and (new.role <> old.role or new.is_blocked <> old.is_blocked) then
    raise exception 'Not allowed';
  end if;
  return new;
end $$;

create trigger protect_profile before update on public.profiles
  for each row execute function public.protect_profile_columns();

-- 3) restaurant owners can't re-activate a restaurant the admin deactivated
create or replace function public.protect_restaurant_columns() returns trigger
language plpgsql as $$
begin
  if auth.uid() is not null and not public.is_admin() and new.is_active <> old.is_active then
    raise exception 'Not allowed';
  end if;
  return new;
end $$;

create trigger protect_restaurant before update on public.restaurants
  for each row execute function public.protect_restaurant_columns();

-- 4) updated_at on orders
create or replace function public.touch_updated_at() returns trigger
language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end $$;

create trigger orders_touch before update on public.orders
  for each row execute function public.touch_updated_at();

-- ---------- ROW LEVEL SECURITY ---------------------------------------
alter table public.profiles    enable row level security;
alter table public.restaurants enable row level security;
alter table public.categories  enable row level security;
alter table public.menu_items  enable row level security;
alter table public.orders      enable row level security;
alter table public.order_items enable row level security;
alter table public.reviews     enable row level security;
alter table public.reports     enable row level security;

-- profiles
create policy profiles_select on public.profiles for select to authenticated
  using (id = auth.uid() or public.is_admin());
create policy profiles_update on public.profiles for update to authenticated
  using (id = auth.uid() or public.is_admin())
  with check (id = auth.uid() or public.is_admin());

-- restaurants
create policy restaurants_select on public.restaurants for select to authenticated
  using (is_active or owner_id = auth.uid() or public.is_admin());
create policy restaurants_insert on public.restaurants for insert to authenticated
  with check (owner_id = auth.uid() and public.my_role() = 'restaurant');
create policy restaurants_update on public.restaurants for update to authenticated
  using (owner_id = auth.uid() or public.is_admin())
  with check (owner_id = auth.uid() or public.is_admin());
create policy restaurants_delete on public.restaurants for delete to authenticated
  using (public.is_admin());

-- categories
create policy categories_select on public.categories for select to authenticated using (true);
create policy categories_admin  on public.categories for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- menu items
create policy menu_select on public.menu_items for select to authenticated using (true);
create policy menu_write  on public.menu_items for all to authenticated
  using (public.owns_restaurant(restaurant_id) or public.is_admin())
  with check (public.owns_restaurant(restaurant_id) or public.is_admin());

-- orders (customers create/cancel through the functions below)
create policy orders_select on public.orders for select to authenticated
  using (customer_id = auth.uid() or public.owns_restaurant(restaurant_id) or public.is_admin());
create policy orders_update on public.orders for update to authenticated
  using (public.owns_restaurant(restaurant_id) or public.is_admin())
  with check (public.owns_restaurant(restaurant_id) or public.is_admin());

-- order items
create policy order_items_select on public.order_items for select to authenticated
  using (public.can_see_order(order_id));

-- reviews
create policy reviews_select on public.reviews for select to authenticated using (true);
create policy reviews_insert on public.reviews for insert to authenticated
  with check (
    customer_id = auth.uid()
    and exists (select 1 from public.orders o
                where o.id = order_id and o.customer_id = auth.uid()
                  and o.restaurant_id = reviews.restaurant_id and o.status = 'completed')
  );
create policy reviews_delete on public.reviews for delete to authenticated using (public.is_admin());

-- reports
create policy reports_insert on public.reports for insert to authenticated
  with check (reporter_id = auth.uid());
create policy reports_select on public.reports for select to authenticated
  using (reporter_id = auth.uid() or public.is_admin());
create policy reports_update on public.reports for update to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- ---------- RPC FUNCTIONS (called from the app) ----------------------
-- Place an order: prices are read from the DB, so the app can't fake totals.
create or replace function public.place_order(
  p_restaurant_id uuid, p_order_type text, p_note text, p_items jsonb
) returns uuid
language plpgsql security definer set search_path = public as $$
declare
  v_uid   uuid := auth.uid();
  v_order uuid;
  v_prof  public.profiles;
  v_item  public.menu_items;
  v_el    jsonb;
  v_qty   int;
  v_count int := 0;
begin
  if v_uid is null then raise exception 'Please sign in first'; end if;
  select * into v_prof from public.profiles where id = v_uid;
  if v_prof.is_blocked then raise exception 'Your account is blocked'; end if;
  if p_order_type not in ('dine_in','pickup') then raise exception 'Invalid order type'; end if;
  if not exists (select 1 from public.restaurants
                 where id = p_restaurant_id and is_active and is_open) then
    raise exception 'This restaurant is not accepting orders right now';
  end if;

  insert into public.orders (customer_id, restaurant_id, order_type, note, customer_name, customer_phone)
  values (v_uid, p_restaurant_id, p_order_type, nullif(trim(p_note), ''), v_prof.full_name, v_prof.phone)
  returning id into v_order;

  for v_el in select value from jsonb_array_elements(p_items) loop
    v_qty := greatest((v_el->>'quantity')::int, 1);
    select * into v_item from public.menu_items
      where id = (v_el->>'menu_item_id')::uuid
        and restaurant_id = p_restaurant_id and is_available;
    if not found then raise exception 'An item in your cart is no longer available'; end if;
    insert into public.order_items (order_id, menu_item_id, item_name, unit_price, quantity)
    values (v_order, v_item.id, v_item.name, v_item.price, v_qty);
    v_count := v_count + 1;
  end loop;

  if v_count = 0 then raise exception 'Your cart is empty'; end if;

  update public.orders
     set total = (select sum(unit_price * quantity) from public.order_items where order_id = v_order)
   where id = v_order;
  return v_order;
end $$;

-- Customer cancels (only while the order is still pending)
create or replace function public.cancel_order(p_order_id uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
  update public.orders set status = 'cancelled'
   where id = p_order_id and customer_id = auth.uid() and status = 'pending';
  if not found then raise exception 'Only pending orders can be cancelled'; end if;
end $$;

-- Admin dashboard numbers
create or replace function public.admin_stats() returns jsonb
language plpgsql stable security definer set search_path = public as $$
begin
  if not public.is_admin() then raise exception 'Admins only'; end if;
  return jsonb_build_object(
    'customers',   (select count(*) from public.profiles where role = 'customer'),
    'restaurants', (select count(*) from public.restaurants),
    'orders',      (select count(*) from public.orders),
    'revenue',     coalesce((select sum(total) from public.orders where status = 'completed'), 0),
    'by_status',   coalesce((select jsonb_object_agg(status, c)
                             from (select status, count(*) c from public.orders group by status) s), '{}'::jsonb)
  );
end $$;

-- ---------- PERMISSIONS ----------------------------------------------
grant usage on schema public to anon, authenticated;
grant select, insert, update, delete on all tables in schema public to authenticated;
grant execute on all functions in schema public to authenticated;

-- ---------- REALTIME --------------------------------------------------
alter publication supabase_realtime add table public.orders;

-- ---------- SEED DATA -------------------------------------------------
insert into public.categories (name) values
  ('Rice & Curry'), ('Kottu'), ('Burgers'), ('Pizza'), ('Short Eats'),
  ('Noodles'), ('Beverages'), ('Desserts');

-- ---------------------------------------------------------------------
-- MAKE YOURSELF ADMIN (after registering once in the app):
--   update public.profiles set role = 'admin'
--   where id = (select id from auth.users where email = 'your-email@example.com');
-- Then log out and log in again.
-- ---------------------------------------------------------------------
