package org.overture.ego.controller;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.entity.Group;
import org.overture.ego.security.ProjectCodeScoped;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/groups")
public class GroupController {

    @ProjectCodeScoped
    @RequestMapping(method = RequestMethod.GET, value = "")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "List of groups", response = Group.class, responseContainer = "List")
            }
    )
    public @ResponseBody
    List<Group> getGroupsList(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
            @RequestParam(value = "offset", required = true) long offset,
            @RequestParam(value = "count", required = false) short count) {
        return null;
    }

    @ProjectCodeScoped
    @RequestMapping(method = RequestMethod.GET, value = "/search")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "List of groups", response = Group.class, responseContainer = "List")
            }
    )
    public @ResponseBody
    List<Group> findGroups(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
            @RequestParam(value = "query", required = true) String query,
            @RequestParam(value = "count", required = false) short count) {
        return null;
    }

    @ProjectCodeScoped
    @RequestMapping(method = RequestMethod.POST, value = "")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "New Group", response = Group.class)
            }
    )
    public @ResponseBody
    Group createGroup(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
            @RequestBody(required = true) Group groupInfo) {
        return null;
    }


    @ProjectCodeScoped
    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Group Details", response = Group.class)
            }
    )
    public @ResponseBody
    Group getGroup(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
            @PathVariable(value = "id", required = true) String groupId) {
        return null;
    }


    @ProjectCodeScoped
    @RequestMapping(method = RequestMethod.PUT, value = "/{id}")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Updated group info", response = Group.class)
            }
    )
    public @ResponseBody
    Group updateGroup(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
            @PathVariable(value = "id", required = true) String groupId,
            @RequestBody(required = true) Group updatedGroupInfo) {
        return null;
    }

    @ProjectCodeScoped
    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteGroup(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
            @PathVariable(value = "id", required = true) String groupId) {
    }

}
