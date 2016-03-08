// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// helpers
import { ReasonType } from '../../../lib/reason-utils';
import { numberize } from '../../../lib/text-utils';

// components
import modal from '../../modal/wrapper';
import ContentBox from '../../content-box/content-box';
import SaveCancel from '../../common/save-cancel';
import { CancelReason } from '../../fields';


@modal
export default class CancelModal extends React.Component {

  static propTypes = {
    entity: PropTypes.string.isRequired,
    count: PropTypes.number.isRequired,
    label: PropTypes.string,
    onCancel: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
  };

  state = {
    reason: null
  };

  render() {
    const {entity, count, label: rawLabel, onCancel, onConfirm} = this.props;

    const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;
    const entityForm = numberize(entity, count);

    const label = rawLabel
      ? rawLabel
      : <span>Are you sure you want to cancel <b>{count} {numberize(entity, count)}</b>?</span>;

    return (
      <ContentBox title={`Cancel ${_.capitalize(entityForm)}?`}
                  className="fc-bulk-action-modal"
                  actionBlock={actionBlock}>
        <div className="fc-modal-body">
          {label}
          <CancelReason reasonType={ReasonType.CANCELLATION}
                        className="fc-modal-cancel-reason"
                        value={this.state.reason}
                        onChange={(reason) => this.setState({reason})} />
        </div>
        <SaveCancel className="fc-modal-footer"
                    cancelTabIndex="2"
                    cancelClassName="fc-modal-close"
                    cancelText="No"
                    onCancel={onCancel}
                    saveTabIndex="1"
                    onSave={() => onConfirm(this.state.reason)}
                    saveText="Yes, Cancel"
                    saveDisabled={this.state.reason === null} />
      </ContentBox>
    );
  }
}
