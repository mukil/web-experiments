
define(['d3'], function (d3, require) {

    function restClient () {}

    function fetch(resource, callback, failure, json, debug) {
        var response_type = "text/plain"
        if (json) response_type = "application/json"
        var xhr = d3.xhr(resource, response_type)
            xhr.get()
            xhr.on('load', function (response) {
                var result = undefined
                if (response !== null) {
                    result = response
                    if (debug) console.log(response.status, response)
                    if (response.status === 200) {
                        // process response
                        if (json) {
                            result = JSON.parse(response.response)
                        } else {
                            result = response.response
                        }
                    }
                    if (typeof callback === "function") callback(result)
                }
            })
            xhr.on('error', function (error) {
                if (typeof failure === "function") failure(error)
            })
    }
    
    function mark(resource, callback, fail) {
        var xhr = d3.xhr(resource)
            xhr.get()
            xhr.on('load', callback)
            xhr.on('error', function (e){
                if (typeof fail !== "undefined") fail(e)                
            })
    }
    
    function post(resource, data, callback, fail, json, debug) {
        var response_type = "application/json"
        var xhr = d3.xhr(resource, response_type)
            xhr.header('Content-Type', "application/json")
            if (data) {
                xhr.post(JSON.stringify(data))
            } else {
                xhr.post()
            }
            xhr.on('load', function (response) {
                if (debug) console.log(response)
                if (json && typeof callback !== "undefined") {
                    // process response
                    var result = undefined
                        result = JSON.parse(response.response)
                    if (debug) console.log(response.status, result)
                    if (response.status !== 200 && response.status !== 204) throw Error(response.status)
                    if (typeof callback !== "function") throw Error("Please always specify a response "
                        + "handler when calling restClient for async HTTP")
                    callback(result)
                } else if (typeof callback !== "undefined") {
                    callback()
                }
            })
            xhr.on('error', function (e) {
                console.warn(e.status, e.statusText)
                if (typeof fail !== "undefined") fail(e)
            })
    }
    
    function authenticate(username, passwd, handle, fail, debug) {
        var xhr = d3.xhr('/accesscontrol/login')
        var auth_code = authorization()
            xhr.header('Content-Type', "application/json")
            xhr.header('Authorization', auth_code)
            xhr.post()
            xhr.on('load', function (response) {
                if (debug) console.log(response)
                if (typeof handle !== "undefined") handle(response)

            })
            xhr.on('error', function (e) {
                console.warn(e.status, e.statusText)
                if (typeof fail !== "undefined") fail(e)
            })
            
            /** Returns value for the "Authorization" header. */
            function authorization() {
                var key = "-SHA256-"
                var code = btoa(username) // ### FIXME: btoa() might not work in IE
                if (passwd !== "") {
                    key += SHA256(passwd)
                    code = btoa(username + ":" + key)
                }
                return "Basic " + code
            }
    }

    restClient.prototype = {

        fetchUsername: function (handle, debug) {
            fetch('/accesscontrol/user', handle, undefined, false, false)
        },
        fetchParticipant: function (handle, failure, debug) {
            if (debug) console.log(" restc: requesting participant ... ")
            fetch('/experiment/participant', handle, failure, true, debug)
        },
        fetchAllIcons: function (handle, debug) {
            // 
            fetch('/experiment/symbol/all', handle, undefined, true, debug)
        },
        fetchAllTrials: function (handle, debug) {
            fetch('/experiment/screen/all', handle, undefined, true, debug)
        },
        fetchAllUnseenTrials: function (conditionUri, handle, debug) {
            // 
            fetch('/experiment/screen/unseen/' + conditionUri, handle, undefined, true, debug)
        },
        fetchTrialConfig: function (trialId, handle, debug) {
            //
            fetch('/experiment/screen/' + trialId, handle, undefined, true, debug)
        },
        fetchNextEstimationNr: function (trialId, handle, fail, debug) {
            //
            fetch('/experiment/estimation/next/' + trialId, handle, fail, true, debug)
        },
        postEstimationReport: function (trialId, estimationNr, payload, handle, fail, debug) {
            post('/experiment/estimation/' + trialId + '/' + estimationNr, payload, handle, fail, false, debug)
        },
        postPinningReport: function (trialId, payload, handle, fail, debug) {
            post('/experiment/pinning/' + trialId, payload, handle, fail, false, debug)
        },
        doMarkTrialAsSeen: function (trialId, callback, fail) {
            mark('/experiment/screen/' + trialId + "/seen", callback, fail)
        },
        logoutParticipant: function (callback, fail) {
            post('/accesscontrol/logout', undefined, callback, fail)
        },
        doMarkIconPreference: function (fileTopicId, callback, fail) {
            mark('/experiment/symbol/choose/' + fileTopicId, callback, fail)
        },
        startSession: function (id, handle, debug) {
            authenticate(id, "", handle, false, debug)
        }

    }

    return restClient
    
})
