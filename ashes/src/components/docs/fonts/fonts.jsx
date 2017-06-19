// @flow

import React from 'react';

import s from './fonts.css';

export const Fonts = () => {
  return (
    <div className={s.block}>
      <div className={s.title}>
        Page Title and <span>Subtitle</span>: 300 30px/41px.
      </div>

      <div className={s.section}>
        SectionTitle → 600 16px/22px.
      </div>

      <div className={s.buttons}>
        Buttons & Field Labels → 600 14px/20px.
      </div>

      <div className={s.nav}>
        Tab Nav → 400 14px/20px.
      </div>

      <div className={s.body}>
        Body → 400 13px/18px.
      </div>
    </div>
  );
};
