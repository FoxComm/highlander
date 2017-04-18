// @flow

// libs
import React, { Component, Element } from 'react';

// components
import wrapModal from 'components/modal/wrapper';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/common/save-cancel';

type Props = {
  onCancel: Function,
  onSave: Function
};

class DeletePropertyModal extends Component {
  props: Props;

  get closeAction(): Element<*> {
    return <a onClick={this.props.onCancel}>&times;</a>;
  }

  render() {
    return(
      <div className="fc-product-details__custom-property">
        <div className="fc-modal-container">
          <ContentBox title="Delete Custom Property?" actionBlock={this.closeAction}>
            <p>Are you sure you want to delete the custom property?</p>
              <SaveCancel
                onCancel={this.props.onCancel}
                onSave={this.props.onSave}
                saveText="Yes, Delete" />
          </ContentBox>
        </div>
      </div>
    );
  }
}

export default wrapModal(DeletePropertyModal);
