// libs
import React, { PropTypes } from 'react';

//helpers
import { inflect } from '../../../lib/text-utils';

// components
import modal from '../../modal/wrapper';
import ContentBox from '../../content-box/content-box';
import { PrimaryButton, CloseButton } from '../../common/buttons';
import { Dropdown, DropdownItem } from '../../dropdown';

@modal
export default class CancelOrder extends React.Component {

  static propTypes = {
    reasons: PropTypes.array.isRequired,
    onCancel: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
    count: PropTypes.number,
  };

  state = {
    reason: null
  };

  render() {
    const {reasons, count, onCancel, onConfirm} = this.props;

    const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;

    return (
      <ContentBox title="Cancel Orders?"
                  className="fc-address-form-modal"
                  actionBlock={actionBlock}>
        <div className='fc-modal-body'>
          Are you sure you want to cancel <b>{count} {inflect(count, 'order', 'orders')}</b>?
          <div className="fc-order-cancel-reason">
            <div>
              <label>
                Cancel Reason
                <span className="fc-order-cancel-reason-asterisk">*</span>
              </label>
            </div>
            <Dropdown className="fc-order-cancel-reason-selector"
                      name="cancellationReason"
                      placeholder="- Select -"
                      value={this.state.reason}
                      onChange={(reason) => this.setState({reason})}>
              {reasons.map(({id, body}) => (
                <DropdownItem key={id} value={id}>{body}</DropdownItem>
              ))}
            </Dropdown>
          </div>
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
