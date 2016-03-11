import React, { PropTypes } from 'react';
import { Link } from '../link';

const NewGroupBase = ({title, alternative, children}) => {
  return (
    <div className='fc-customer-group-new'>
      <div className='fc-grid'>
        <header className='fc-customer-form-header fc-col-md-1-1'>
          <h1 className='fc-title'>
            {title}
          </h1>
          {alternative ? (
            <span>
              or <Link className='fc-customer-group-new-or gc-customer-group-new-title' to={alternative.id}>
              create a {alternative.title}
            </Link>
            </span>
          ) : null}
        </header>
        <article className='fc-col-md-1-1'>
          {children}
        </article>
      </div>
    </div>
  );
};

NewGroupBase.propTypes = {
  title: PropTypes.string.isRequired,
  alternative: PropTypes.shape({
    id: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
  }),
  children: PropTypes.node
};

export default NewGroupBase;
