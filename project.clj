(defproject clojure-msgpack "0.1.1-SNAPSHOT"
  :description "Pure Clojure implementation of MessagePack spec."
  :url "https://github.com/edma2/clojure-msgpack"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [reiddraper/simple-check "0.5.3"]])

(cemerick.pomegranate.aether/register-wagon-factory!
   "scp" #(let [c (resolve 'org.apache.maven.wagon.providers.ssh.external.ScpExternalWagon)]
                      (clojure.lang.Reflector/invokeConstructor c (into-array []))))

