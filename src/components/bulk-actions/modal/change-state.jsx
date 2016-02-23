// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// helpers
import { numberize } from '../../../lib/text-utils';

// components
import modal from '../../modal/wrapper';
import ContentBox from '../../content-box/content-box';
import SaveCancel from '../../common/save-cancel';


@modal
export default class ChangeStateModal extends React.Component {

  static propTypes = {
    entity: PropTypes.string.isRequired,
    stateTitle: PropTypes.string.isRequired,
    count: PropTypes.number,
    onCancel: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
  };

  render() {
    const {entity, stateTitle, count, onCancel, onConfirm} = this.props;

    const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;
    const entityForm = numberize(entity, count);

    return (
      <ContentBox title={`Change ${_.capitalize(entityForm)} state to ${stateTitle}?`}
                  className="fc-bulk-action-modal"
                  actionBlock={actionBlock}>
        <div className='fc-modal-body'>
          Are you sure you want to change the state to <b>{stateTitle}</b> for <b>{count} {entityForm}</b>?
        </div>
        <SaveCancel className="fc-modal-footer"
                    cancelTabIndex="2"
                    cancelClassName="fc-modal-close"
                    cancelText="No"
                    onCancel={onCancel}
                    saveTabIndex="1"
                    onSave={onConfirm}
                    saveText="Yes, Change State" />
      </ContentBox>
    );
  }
}
