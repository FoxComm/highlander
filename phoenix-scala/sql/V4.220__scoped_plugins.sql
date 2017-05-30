alter table plugins add column scope exts.ltree;

update plugins set scope = exts.text2ltree(get_scope_path((
        select scope_id from organizations
            inner join account_organizations as aco on (organizations.id = aco.organization_id)
            inner join users as u on (u.account_id = aco.account_id)
          where u.email = 'api@foxcommerce.com'
      ))::text);

update plugins set scope = exts.text2ltree('1'::text) where scope is null;

alter table plugins alter column scope set not null;
