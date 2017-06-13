// libs
import React from 'react';

// components
import SvgIcon from 'components/core/svg-icon';

// styles
import s from './logos.css';

const logoSet = [
  'fox',
  'start',
  'logo',
];

const Logos = () => {
  const icons = logoSet.map((logoType) => (
    <div key={logoType} className={s.box}>
      <div className={s.iconWrapper}>
        <SvgIcon name={logoType} className={s.svgStyle} />
      </div>
      <span className={s.text}>{logoType}</span>
    </div>
  ));

  return (
    <div className={s.container}>
      {icons}
    </div>
  );
};

export default Logos;
