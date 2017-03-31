insert into plugins
    (name, version, description, is_disabled, api_host, api_port, settings, schema_settings, created_at, updated_at, deleted_at)
    values ('imgix', '1.0', 'Imgix CDN integration plugin', false, null, null,
    '{"s3_bucket": "", "s3_prefix": "", "cdn_prefix": ""}',
    '[{"name": "cdn_prefix", "type": "string", "title": "CDN url prefix", "default": ""},' ||
    ' {"name": "s3_bucket", "type": "string", "title": "S3 bucket", "default": ""}, ' ||
    '{"name": "s3_prefix", "type": "string", "title": "S3 prefix", "default": "albums"}]',
    '2017-03-28 23:04:50.896000', null, null);