alter table Events
alter column payload type json using to_jsonb(payload)::json;
