import React from 'react';
import PropTypes from 'prop-types';
import DescriptiveGridColumn from './descriptive-grid-column';

const StyleGuideGridGenerator = props => {
  let rows = [];
  for (var i = 0; i < 12; i++) {
    rows.push(<DescriptiveGridColumn size={ props.size } numerator="1" denominator="12" />);
  }

  return (
    <div>
      <div className='fc-grid'>
        <div className='fc-col-sm-1-1'>
          <h2>{ props.name }</h2>
          <p>{ props.description }</p>
        </div>
      </div>
      <div className='fc-grid'>
        {rows}
      </div>
      <div className='fc-grid'>
        <DescriptiveGridColumn size={ props.size } numerator="1" denominator="6" />
        <DescriptiveGridColumn size={ props.size } numerator="5" denominator="6" />
      </div>
      <div className='fc-grid'>
        <DescriptiveGridColumn size={ props.size } numerator="1" denominator="4" />
        <DescriptiveGridColumn size={ props.size } numerator="3" denominator="4" />
      </div>
      <div className='fc-grid'>
        <DescriptiveGridColumn size={ props.size } numerator="1" denominator="3" />
        <DescriptiveGridColumn size={ props.size } numerator="2" denominator="3" />
      </div>
      <div className='fc-grid'>
        <DescriptiveGridColumn size={ props.size } numerator="5" denominator="12" />
        <DescriptiveGridColumn size={ props.size } numerator="7" denominator="12" />
      </div>
      <div className='fc-grid'>
        <DescriptiveGridColumn size={ props.size } numerator="1" denominator="2" />
        <DescriptiveGridColumn size={ props.size } numerator="1" denominator="2" />
      </div>
      <div className='fc-grid'>
        <DescriptiveGridColumn size={ props.size } numerator="1" denominator="1" />
      </div>
    </div>
  );
};

StyleGuideGridGenerator.propTypes = {
  size: PropTypes.number,
  name: PropTypes.string,
  description: PropTypes.string
};

const StyleGuideGrid = props => {
  return (
    <div>
      <StyleGuideGridGenerator
        name="Small Grid"
        description="The column spacing in the small grid will stay static for
                     any browser size. It will not stack even on the phone."
        size="sm" />
      <StyleGuideGridGenerator
        name="Medium Grid"
        description="The column spacing in the medium grid will stay static for
                     any browser greater than or equal to 768px. Below that, it will stack."
        size="md" />
      <StyleGuideGridGenerator
        name="Large Grid"
        description="The column spacing in the large grid will stay static for
                     any browser 1280px and larger. Below that, it will stack."
        size="lg" />
      <StyleGuideGridGenerator
        name="Extra Large Grid"
        description="The column spacing in the extra large grid will stay static
                     for any browser greater than or equal to 1441px. Below that, it will stack."
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
};

StyleGuideGrid.propTypes = {
  size: PropTypes.number
};

export default StyleGuideGrid;
