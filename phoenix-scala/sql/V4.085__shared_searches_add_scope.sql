alter table shared_searches add column access_scope exts.ltree;

update shared_searches set access_scope = exts.text2ltree(get_scope_path((
        select scope_id from organizations
            inner join account_organizations as aco on (organizations.id = aco.organization_id)
          where aco.account_id = shared_searches.store_admin_id
      ))::text);

alter table shared_searches alter column access_scope set not null;
