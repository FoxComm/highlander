
import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';
import Title from './title';

const SectionTitle = (props) => {
  return (
    <div className="fc-grid fc-section-title">
      <div className="fc-col-md-2-6">
        <Title title={ props.title } subtitle={ props.subtitle } />
      </div>
      <div className="fc-col-md-2-6 fc-push-md-2-6 fc-section-title-actions">
        {props.buttonClickHandler && (
          <PrimaryButton icon="add" onClick={props.buttonClickHandler}>
            {props.title}
          </PrimaryButton>
        )}
        {props.children}
      </div>
    </div>
  );
};

SectionTitle.propTypes = {
  title: PropTypes.node,
  subtitle: PropTypes.node,
  buttonClickHandler: PropTypes.func,
  children: PropTypes.node
};

export default SectionTitle;
