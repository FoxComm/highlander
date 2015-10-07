'use strict';

import React from 'react';
import { Link } from '../link';

export default class StyleGuide extends React.Component {
	render() {
		return (
			<div className='fc-style-guide'>
				<div><Link to='style-guide' className='style-guide' /></div>
				<div>This is the style guide</div>
				<h2>Grid</h2>
				<div className='fc-grid'>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
					<div className='fc-col-1-12'>
						<div className='content-box'>
							fc-col-1-12
						</div>
					</div>
				</div>
				<div className='fc-grid'>
					<div className='fc-col-1-6'>
						<div className='content-box'>
							fc-col-1-6
						</div>
					</div>
					<div className='fc-col-5-6'>
						<div className='content-box'>
							fc-col-5-6
						</div>
					</div>
				</div>
				<div className='fc-grid'>
					<div className='fc-col-1-4'>
						<div className='content-box'>
							fc-col-1-4
						</div>
					</div>
					<div className='fc-col-3-4'>
						<div className='content-box'>
							fc-col-3-4
						</div>
					</div>
				</div>
				<div className='fc-grid'>
					<div className='fc-col-1-3'>
						<div className='content-box'>
							fc-col-1-3
						</div>
					</div>
					<div className='fc-col-2-3'>
						<div className='content-box'>
							fc-col-2-3
						</div>
					</div>
				</div>
				<div className='fc-grid'>
					<div className='fc-col-5-12'>
						<div className='content-box'>
							fc-col-5-12
						</div>
					</div>
					<div className='fc-col-7-12'>
						<div className='content-box'>
							fc-col-7-12
						</div>
					</div>
				</div>
				<div className='fc-grid'>
					<div className='fc-col-1-2'>
						<div className='content-box'>
							fc-col-1-2
						</div>
					</div>
					<div className='fc-col-1-2'>
						<div className='content-box'>
							fc-col-1-2
						</div>
					</div>
				</div>
				<div className='fc-grid'>
					<div className='fc-col-1-1'>
						<div className='content-box'>
							fc-col-1-1
						</div>
					</div>
				</div>
			</div>
		);
	}
}
