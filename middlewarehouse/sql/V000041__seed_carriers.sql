-- delete from carriers;

insert into carriers (id, scope, name, tracking_template) values
  -- (1, '1.2', 'USPS', 'https://tools.usps.com/go/TrackConfirmAction_input?qtc_tLabels1=%s');
  -- (2, '1.2', 'FedEx', 'http://www.fedex.com/Tracking?action=track&tracknumbers=%s'),
  (3, '1.2', 'UPS', 'http://wwwapps.ups.com/WebTracking/track?track=yes&trackNums=%s');
