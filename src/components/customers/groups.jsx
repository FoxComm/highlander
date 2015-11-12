import React from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerGroups extends React.Component {

  render() {
    let actionBlock = (
      <button className="fc-btn">
        <i className="icon-add"></i>
      </button>
    );
    return (
      <ContentBox title="Groups"
                  className="fc-customer-groups"
                  actionBlock={ actionBlock }>
        Groups
      </ContentBox>
    );
  }
}
