import aws from 'aws-sdk';

export default function *s3sign(next) {
  if (!this.request.url.match(/\/s3sign/)) {
    yield next;
  } else {
    const s3 = new aws.S3();
    const { name, type } = this.request.query;
    const bucket = process.env.S3_BUCKET;

    const s3Params = {
      Bucket: bucket,
      Key: name,
      Expires: 60,
      ContentType: type,
      ACL: 'public-read',
    };

    s3.getSignedUrl('putObject', s3Params, (err, data) => {
      if (err) {
        this.status = 500;

        return;
      }

      this.body = {
        signedRequest: data,
        url: `https://${bucket}.s3.amazonaws.com/${name}`,
      };
    });
  }
}
