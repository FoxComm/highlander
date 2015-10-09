'use strict';

import React from 'react';
import { Link } from '../link';
import DescriptiveGridColumn from './descriptive-grid-column';

export default class StyleGuide extends React.Component {
  getGrid(size) {
    let rows = [];
    for (var i = 0; i < 12; i++) {
      rows.push(<DescriptiveGridColumn size={ size } numerator="1" denominator="12" />);
    }

    return (
      <div>
        <div className='fc-grid'>
          {rows}
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ size } numerator="1" denominator="6" />
          <DescriptiveGridColumn size={ size } numerator="5" denominator="6" />
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ size } numerator="1" denominator="4" />
          <DescriptiveGridColumn size={ size } numerator="3" denominator="4" />
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ size } numerator="1" denominator="3" />
          <DescriptiveGridColumn size={ size } numerator="2" denominator="3" />
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ size } numerator="5" denominator="12" />
          <DescriptiveGridColumn size={ size } numerator="7" denominator="12" />
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ size } numerator="1" denominator="2" />
          <DescriptiveGridColumn size={ size } numerator="1" denominator="2" />
        </div>
        <div className='fc-grid'>
          <DescriptiveGridColumn size={ size } numerator="1" denominator="1" />
        </div>
      </div>
      );
  }

  render() {
    return (
      <div className='fc-style-guide'>
        <div><Link to='style-guide' className='style-guide' /></div>
        <div className='fc-grid'>
          <div className='fc-col-sm-1-1'>
            <h2>Small Grid</h2>
            <p>
              The column spacing in the small grid will stay static for any browser
              size. It will not stack even on the phone.
            </p>
          </div>
        </div>
        { this.getGrid('sm') }
        <div className='fc-grid'>
          <div className='fc-col-sm-1-1'>
            <h2>Medium Grid</h2>
            <p>
              The column spacing in the medium grid will stay static for any browser
              greater than or equal to 768px. Below that, it will stack.
            </p>
          </div>
        </div>
        { this.getGrid('md') }
        <div className='fc-grid'>
          <div className='fc-col-md-1-1'>
            <h2>Large Grid</h2>
            <p>
              The column spacing in the large grid will stay static for any browser
              1280px and larger. Below that, it will stack.
            </p>
          </div>
        </div>
        { this.getGrid('lg') }
        <div className='fc-grid'>
          <div className='fc-col-md-1-1'>
            <h2>Extra Large Grid</h2>
            <p>
              The column spacing in the large grid will stay static for any browser
              greater than or equal to 1441px. Below that, it will stack.
            </p>
          </div>
        </div>
        { this.getGrid('xl') }
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
