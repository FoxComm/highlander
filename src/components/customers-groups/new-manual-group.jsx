import React, { PropTypes } from 'react';
import NewGroupBase from './new-group-base.jsx';

export default class NewManualGroup extends React.Component {
  render () {
    return (
      <NewGroupBase title='New Manual Customer Group'
                  alternativeId='groups-new-dynamic'
                  alternativeTitle='dynamic group'/>
    );
  }
}
