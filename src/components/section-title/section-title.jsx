
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { PrimaryButton } from '../common/buttons';
import Title from './title';

const SectionTitle = props => {
  return (
    <div className={ classNames('fc-grid fc-section-title', props.className) }>
      <div className="fc-col-md-2-3">
        <Title title={ props.title } subtitle={ props.subtitle } isPrimary={ props.isPrimary } />
      </div>
      <div className="fc-col-md-1-3 fc-section-title-actions">
        {props.onAddClick && (
          <PrimaryButton icon="add" onClick={props.onAddClick}>
            {props.addTitle}
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
  addTitle: PropTypes.node,
  onAddClick: PropTypes.func,
  children: PropTypes.node,
  className: PropTypes.string,
  isPrimary: PropTypes.bool
};

SectionTitle.defaultProps = {
  isPrimary: true
};

export default SectionTitle;
