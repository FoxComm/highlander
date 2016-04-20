provider "google" 
{
    credentials = "${file("account.json")}"
    project = "foxcomm-staging"
    region = "us-central1"
}
