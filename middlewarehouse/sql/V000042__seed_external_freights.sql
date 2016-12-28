delete from external_freights;

insert into external_freights (id, carrier_id, method_name, service_code) values
  (1, 1, 'UPS Standard', '11'),
  (2, 1, 'UPS Ground', '03'),
  (3, 1, 'UPS 3 Day Select', '12'),
  (4, 1, 'UPS 2nd Day Air', '02'),
  (5, 1, 'UPS 2nd Day Air AM', '59'),
  (6, 1, 'UPS Next Day Air Saver', '13'),
  (7, 1, 'UPS Next Day Air', '01'),
  (8, 1, 'UPS Next Day Air Early A.M.', '14'),
  (9, 1, 'UPS Worldwide Express', '07'),
  (10, 1, 'UPS Worldwide Express Plus', '54'),
  (11, 1, 'UPS Worldwide Expedited', '08'),
  (12, 1, 'UPS Worldwide Saver', '65');
