// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// helpers
import { numberize } from '../../../lib/text-utils';

// components
import modal from '../../modal/wrapper';
import ContentBox from '../../content-box/content-box';
import { PrimaryButton, CloseButton } from '../../common/buttons';


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
        <div className='fc-modal-footer'>
          <a tabIndex="2" className="fc-modal-close" onClick={onCancel}>
            No
          </a>
          <PrimaryButton tabIndex="1"
                         autoFocus={true}
                         onClick={onConfirm}>
            Yes, Change State
          </PrimaryButton>
        </div>
      </ContentBox>
    );
  }
}
