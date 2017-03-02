import React from 'react';
import classnames from 'classnames';


const Icon = ({name, className})=>{
  return(
    <svg className={className}>
      <use xlinkHref={'#'+name} />
    </svg>
  );
};
// name would be provided in the format: fc-"your name here"-icon
export default Icon;
