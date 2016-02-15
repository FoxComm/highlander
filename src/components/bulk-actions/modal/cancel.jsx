// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// helpers
import { ReasonType } from '../../../lib/reason-utils';
import { capitalize, inflect } from '../../../lib/text-utils';

// components
import modal from '../../modal/wrapper';
import ContentBox from '../../content-box/content-box';
import { PrimaryButton, CloseButton } from '../../common/buttons';
import { CancelReason } from '../../fields';


@modal
export default class CancelModal extends React.Component {

  static propTypes = {
    entityForms: PropTypes.arrayOf(PropTypes.string).isRequired,
    count: PropTypes.number,
    onCancel: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
  };

  state = {
    reason: null
  };

  render() {
    const {entityForms, count, onCancel, onConfirm} = this.props;

    const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;
    const entityForm = inflect(count, entityForms);

    return (
      <ContentBox title={`Cancel ${capitalize(entityForm)}?`}
                  className="fc-bulk-action-modal"
                  actionBlock={actionBlock}>
        <div className='fc-modal-body'>
          Are you sure you want to cancel <b>{count} {entityForm}</b>?
          <CancelReason reasonType={ReasonType.CANCELLATION}
                        className="fc-modal-cancel-reason"
                        value={this.state.reason}
                        onChange={(reason) => this.setState({reason})} />
        </div>
        <div className='fc-modal-footer'>
          <a tabIndex="2" className="fc-modal-close" onClick={onCancel}>
            No
          </a>
          <PrimaryButton tabIndex="1"
                         autoFocus={true}
                         disabled={this.state.reason === null}
                         onClick={() => onConfirm(this.state.reason)}>
            Yes, Cancel
          </PrimaryButton>
        </div>
      </ContentBox>
    );
  }
}
