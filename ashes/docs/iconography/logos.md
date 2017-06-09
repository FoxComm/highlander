```
const container = { 
  display: 'flex',
  flexWrap: 'wrap',
  justifyContent: 'center',
};

const box =  {
  display: 'flex',
  flexDirection: 'column',
  margin: '10px',
  color: '#6d859e'
}

const iconWrapper =  {
  width: '200px',
  height: '200px',
  border: '1px dashed #bdc9d6',
  textAlign: 'center',
  lineHeight: '120px',
  fontSize: '28px'
  
}

const text = {
  marginTop: '5px',
  fontSize: '12px',
  textAlign: 'center',
  lineHeight: '15px'
}

const names = [
  "fox",
  "start",
  "logo",
]

const svgStyle = {
  fill: '#6d859e',
  margin: '10px'
}

const LogoIconography = () => { 
  const logos = names.map((name) => (
      <div key={name} style={box}>
        <div style={iconWrapper}> 
          <svg style={svgStyle} viewBox='0 0 19 19'>
            <use xlinkHref={`#icon-${name}`} />
          </svg>
        </div>
        <span style={text}>{name}</span>
      </div>
    ));

  return(
    <div style={container}>
      {logos}
    </div>
  );
};

<LogoIconography />
```
