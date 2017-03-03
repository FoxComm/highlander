/* @flow */
import React from 'react';
import classnames from 'classnames';

const Icon = ({name, className} : {name:string, className?:string})=>{
  return(
    <svg className={className}>
      <use xlinkHref={'#'+ "fc-"+name+"-icon"} />
    </svg>
  );
};
export default Icon;
