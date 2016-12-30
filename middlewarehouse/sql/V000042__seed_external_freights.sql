delete from external_freights;

insert into external_freights (id, carrier_id, method_name, service_code) values
  (1, 3, 'UPS Standard', '11'),
  (2, 3, 'UPS Ground', '03'),
  (3, 3, 'UPS 3 Day Select', '12'),
  (4, 3, 'UPS 2nd Day Air', '02'),
  (5, 3, 'UPS 2nd Day Air AM', '59'),
  (6, 3, 'UPS Next Day Air Saver', '13'),
  (7, 3, 'UPS Next Day Air', '01'),
  (8, 3, 'UPS Next Day Air Early A.M.', '14'),
  (9, 3, 'UPS Worldwide Express', '07'),
  (10, 3, 'UPS Worldwide Express Plus', '54'),
  (11, 3, 'UPS Worldwide Expedited', '08'),
  (12, 3, 'UPS Worldwide Saver', '65');
