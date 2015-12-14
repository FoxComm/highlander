import React, { PropTypes } from 'react';
import { Link } from '../link';

const NewGroupBase = (props) => {
  return (
    <div className='fc-group-new'>
      <div className='fc-grid'>
        <header className='fc-customer-form-header fc-col-md-1-1'>
          <h1 className='fc-title'>
            {props.title}
          </h1>
          <Link className='fc-group-new-or gc-group-new-title' to={props.alternativeId}>
            or create a {props.alternativeTitle}
          </Link>
        </header>
        <article className='fc-col-md-1-1'>
          {props.children}
        </article>
      </div>
    </div>
  );
};

NewGroupBase.propTypes = {
  title: PropTypes.string.isRequired,
  alternativeId: PropTypes.string.isRequired,
  alternativeTitle: PropTypes.string.isRequired,
  children: PropTypes.node
};

export default NewGroupBase;
