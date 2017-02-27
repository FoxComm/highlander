# Adding New Applications To Marathon

To add new application and configure deployment to marathon you will be changing one of marathon roles: `dev/marathon` to add applications and `dev/marathon_consumers` to add new consumer.
Follow next steps to do so:

1. Add application deployment tasks to `dev/marathon` or `dev/marahon_consumers` role in file with name `app_${NEW_APP_NAME}.yml` (in tasks directory).
2. Add deployment configuration template to `dev/marathon` or `dev/marahon_consumers` role in file with name `${NEW_APP_NAME}.json` (in templates directory).
3. Set variables, needed for deployment in `dev/marathon` or `dev/marahon_consumers` role in file with `${NEW_APP_NAME}.yml` (in vars directory).
4. Add deployment instructions to proper section of deployment: `backend.yml`, `consumers.yml` or `frontend.yml` in `dev/marathon` or `dev/marahon_consumers` role.
5. Modify branch configuration in `group_vars/all` file. It should contain `${NEW_APP_NAME}: master` in `docker_tags` section.
6. Modify branch configuration in `env.local.j2` template of `dev/env_local` role. It should contain `export ${DOCKER_TAG_${NEW_APP_NAME}:=master}`.
7. Modify branch configuration in `goldrush_appliance.yml` playbook. It should contain `${NEW_APP_NAME}: "{{ lookup('env', 'DOCKER_TAG_${NEW_APP_NAME}') | default('master') }}"` in `docker_tags`\|
 section.
8. Modify configuration of single application deployment in `dev/update_app` role. This step requires changes in `vars/main.yml`:
- you have to add application name to `supported_apps` section;
- you have to add `${NEW_APP_NAME}: "{{tag_name}}"` `docker_tags` section and build steps;
- you have to modify build steps in case of not default build commands and application directory.
