// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// helpers
import { ReasonType } from '../../../lib/reason-utils';
import { inflect } from '../../../lib/text-utils';

// components
import modal from '../../modal/wrapper';
import ContentBox from '../../content-box/content-box';
import { PrimaryButton, CloseButton } from '../../common/buttons';
import { CancelReason } from '../../fields';


@modal
export default class CancelOrder extends React.Component {

  static propTypes = {
    onCancel: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
    count: PropTypes.number,
  };

  state = {
    reason: null
  };

  render() {
    const {count, onCancel, onConfirm} = this.props;

    const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;

    return (
      <ContentBox title="Cancel Orders?"
                  className="fc-address-form-modal"
                  actionBlock={actionBlock}>
        <div className='fc-modal-body'>
          Are you sure you want to cancel <b>{count} {inflect(count, 'order', 'orders')}</b>?
          <CancelReason reasonType={ReasonType.CANCELLATION}
                        className="fc-order-cancel-reason"
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
