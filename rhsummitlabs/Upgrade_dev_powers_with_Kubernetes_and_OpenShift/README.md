# Upgrade dev powers with Kubernetes and OpenShift

## OpenShift

To deploy the content on OpenShift

```
oc new-app osevg/workshopper -e CONTENT_URL_PREFIX="https://raw.githubusercontent.com/openshift-labs/starter-guides/master" -e WORKSHOPS_URLS="https://raw.githubusercontent.com/openshift-labs/starter-guides/master/_workshops/training.yml"

oc expose svc workshopper
```

then navigate to the generated URL.

## Locally

To deploy using local container

```
git clone https://github.com/openshift-labs/starter-guides.git

cd starter-guides

docker run -p 8080:8080 -ti --rm --name=workshops -v `pwd`:/app-data -e CONTENT_URL_PREFIX="file:///app-data" -e WORKSHOPS_URLS="file:///app-data/_workshops/training.yml" osevg/workshopper
```

and open http://localhost:8080 to access the content.
