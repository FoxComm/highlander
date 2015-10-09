'use strict';

import React from 'react';
import { Link } from '../link';
import StyleGuideGrid from './style-guide-grid';

export default class StyleGuide extends React.Component {
  render() {
    return (
      <div className='fc-style-guide'>
        <div><Link to='style-guide' className='style-guide' /></div>
        <StyleGuideGrid
          name="Small Grid"
          description="The column spacing in the small grid will stay static for any browser size. It will not stack even on the phone."
          size="sm" />
        <StyleGuideGrid
          name="Medium Grid"
          description="The column spacing in the medium grid will stay static for any browser greater than or equal to 768px. Below that, it will stack."
          size="md" />
        <StyleGuideGrid
          name="Large Grid"
          description="The column spacing in the large grid will stay static for any browser 1280px and larger. Below that, it will stack."
          size="lg" />
        <StyleGuideGrid
          name="Extra Large Grid"
          description="The column spacing in the extra large grid will stay static for any browser greater than or equal to 1441px. Below that, it will stack."
          size="xl" />
        <div className='fc-grid'>
          <div className='fc-col-sm-1-1'>
            <h2>Variable Grid</h2>
            <p>
              Cells may have multiple sizes based on the width of the browser.
            </p>
          </div>
        </div>
        <div className='fc-grid'>
          <div className='fc-col-sm-1-2 fc-col-md-1-3 fc-col-lg-1-4 fc-col-xl-1-4'>
            <div className='content-box'>
              fc-col-sm-1-2 fc-col-md-1-3 fc-col-lg-1-4 fc-col-xl-1-4
            </div>
          </div>
          <div className='fc-col-sm-1-2 fc-col-md-1-3 fc-col-lg-3-4 fc-col-xl-1-4'>
            <div className='content-box'>
              fc-col-sm-1-2 fc-col-md-1-3 fc-col-lg-3-4 fc-col-xl-1-4
            </div>
          </div>
          <div className='fc-col-sm-1-2 fc-col-md-1-3 fc-col-lg-7-8 fc-col-xl-1-4'>
            <div className='content-box'>
              fc-col-sm-1-2 fc-col-md-1-3 fc-lg-col-7-8 fc-col-xl-1-4
            </div>
          </div>
          <div className='fc-col-sm-1-2 fc-col-md-1-1 fc-col-lg-1-8 fc-col-xl-1-4'>
            <div className='content-box'>
              fc-col-sm-1-2 fc-col-md-1-1 fc-col-lg-1-8 fc-col-xl-1-4
            </div>
          </div>
        </div>
      </div>
    );
  }
}
