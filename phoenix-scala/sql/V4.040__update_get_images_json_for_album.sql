create or replace function get_images_json_for_album(int) returns jsonb as $$
begin
    return json_agg(imgs) from (select 
            (form.attributes ->> (shadow.attributes -> 'src' ->> 'ref')) as src,
            (form.attributes ->> (shadow.attributes -> 'baseUrl' ->> 'ref')) as baseUrl,
            (form.attributes ->> (shadow.attributes -> 'alt' ->> 'ref')) as alt,
            (form.attributes ->> (shadow.attributes -> 'title' ->> 'ref')) as title
    FROM images as image
    INNER JOIN album_image_links as lnk on (lnk.right_id = image.id)
    INNER JOIN object_forms as form on (form.id = image.form_id)
    INNER JOIN object_shadows as shadow on (shadow.id = image.shadow_id)
    WHERE lnk.left_id = $1        
    ORDER BY lnk.position) as imgs;
end;
$$ language plpgsql;
