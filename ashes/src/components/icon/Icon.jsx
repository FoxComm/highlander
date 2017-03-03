import React from 'react';
import classnames from 'classnames';


const Icon = ({name, className})=>{
  return(
    <svg className={className}>
      <use xlinkHref={'#'+ "fc-"+name+"-icon"} />
    </svg>
  );
};
export default Icon;
