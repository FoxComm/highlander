import React, { PropTypes } from 'react';
import NewGroupBase from './new-group-base';

export default class ManualGroup extends React.Component {
  render () {
    return (
      <NewGroupBase title='New Manual Customer Group'
                  alternativeId='groups-new-dynamic'
                  alternativeTitle='dynamic group'/>
    );
  }
}
