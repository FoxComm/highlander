
import React, { PropTypes } from 'react';
import classNames from 'classnames';

import { PrimaryButton } from '../common/buttons';
import Title from './title';

/**
 * SectionTitle simple header with this structure -> [<Title>, <Actions>]
 * Section title is intendent for secondary titles (for example Notes for some gift card)
 */
const SectionTitle = props => {
  return (
    <div className={ classNames('fc-section-title', props.className) }>
      <Title title={ props.title } subtitle={ props.subtitle } tag={props.titleTag}/>
      <div className="fc-section-title__actions">
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
  titleTag: Title.propTypes.title,
};

export default SectionTitle;
