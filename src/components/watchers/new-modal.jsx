
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import { ModalContainer } from '../modal/base';
import ContentBox from '../content-box/content-box';
import { PrimaryButton } from '../common/buttons';

const AddWatcherModal = props => {
  const title = `Assign ${props.entity.entityType}`;
  const text = `${title} to:`;

  const actionBlock = (
    <a className='fc-modal-close' onClick={props.cancelAction}>
      <i className='icon-close'></i>
    </a>
  );

  const footer = (
    <div className="fc-modal-footer fc-add-watcher-modal__footer">
      <a className="fc-btn-link"
         onClick={props.cancelAction}>Cancel</a>
      <PrimaryButton onClick={props.cancelAction}>
        Assign
      </PrimaryButton>
    </div>
  );

  return (
    <ModalContainer isVisible={props.isVisible}>
      <ContentBox title={title}
                  actionBlock={actionBlock}
                  footer={footer}
                  className="fc-add-watcher-modal">
        <div className="fc-modal-body fc-add-watcher-modal__content">
          {text}
        </div>
      </ContentBox>
    </ModalContainer>
  );
};

AddWatcherModal.propTypes = {
  isVisible: PropTypes.bool,
  entity: PropTypes.shape({
    entityType: PropTypes.string,
    entityId: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
  }).isRequired,
  cancelAction: PropTypes.func.isRequired
};

AddWatcherModal.defaultProps = {
  isVisible: false
};

export default AddWatcherModal;
