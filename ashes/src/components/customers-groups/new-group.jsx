import React, { PropTypes } from 'react';
import { Link } from '../link';

//helpers
import { prefix } from '../../lib/text-utils';

const prefixed = prefix('fc-customer-group-dynamic-edit');

const NewGroupBase = ({title, alternative, children}) => {
  return (
    <div className={prefixed()}>
      <header>
        <h1 className="fc-title">
          {title}
        </h1>
        {alternative ? (
          <span>
            or <Link className={prefixed('or')} to={alternative.id}>
            create a {alternative.title}
          </Link>
          </span>
        ) : null}
      </header>
      <article>
        {children}
      </article>
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
