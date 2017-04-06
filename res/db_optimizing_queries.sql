delete from vote where poll_id in (select id from poll where agenda_item_id in (select id from agenda_item where meeting_id < 5596));
delete from poll where agenda_item_id in (select id from agenda_item where meeting_id < 5596);
delete from agenda_item_attachment where agenda_item_id in (select id from agenda_item where meeting_id < 5596);
delete from agenda_item where meeting_id < 5596;
delete from meeting_attachment where meeting_id < 5596;
delete from meeting where id < 5596;

