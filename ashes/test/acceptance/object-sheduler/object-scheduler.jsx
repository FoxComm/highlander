import React from 'react';
import { assoc } from 'sprout-data';
import moment from 'moment';

describe('ObjectScheduler', function() {
  const ObjectScheduler = requireComponent('object-scheduler/object-scheduler.jsx');

  const activeObject = require('./activeObject.json');

  it('should expand activeFrom field if passed object is active', function*() {
    const { container } = yield renderIntoDocument(<ObjectScheduler {...activeObject} />);

    expect(container.querySelector('.fc-product-state__picker-label')).not.to.be.null;
    expect(container.querySelector('.fc-product-state__picker-label').textContent).to.be.equal('Start');
    container.unmount();
  });

  it('should expand date fields if object has activeFrom field, even if object is not active yet', function*() {
    const willBeActive = assoc(activeObject, ['form', 'activeFrom'], moment().add(6, 'days').toISOString());
    const { container } = yield renderIntoDocument(<ObjectScheduler {...willBeActive} />);

    expect(container.querySelector('.fc-product-state__picker-label')).not.to.be.null;
    container.unmount();
  });
});
