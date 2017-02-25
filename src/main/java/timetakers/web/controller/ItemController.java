/*
 * Copyright (c) 2016 David Liebl, Martin Geßenich, Sebastian Pettirsch, Christian Rehaag, Volker Mertens
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package timetakers.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import timetakers.exception.ValidationRuntimeException;
import timetakers.model.Item;
import timetakers.repository.ItemRepository;
import timetakers.repository.specification.ItemSpecification;
import timetakers.services.ItemService;
import timetakers.services.SecurityService;
import timetakers.util.TextKey;
import timetakers.web.assembler.ItemAssembler;
import timetakers.web.model.ItemDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * @author Martin Geßenich
 */

@Controller
@Transactional
@RequestMapping(value = "/item")
public class ItemController {

    private ItemRepository itemRepository;
    private ItemAssembler itemAssembler;
    private ItemService itemService;

    @Autowired
    public ItemController(ItemRepository itemRepository, ItemAssembler itemAssembler, ItemService itemService) {
        this.itemRepository = itemRepository;
        this.itemAssembler = itemAssembler;
        this.itemService = itemService;
    }

    @RequestMapping(value = "/new", method = RequestMethod.POST)
    @ResponseBody
    public void postNewItem( @RequestBody ItemDto itemDto ) {
        Item father = null;
        if (itemDto.father != null) {
            father = itemRepository.getOne(itemDto.father);
        }
        if (StringUtils.isEmpty(itemDto.title)) {
            throw new ValidationRuntimeException(new TextKey("item.validation.emptyTitle"), "title");
        }
        Item item = Item.builder()
                .withTitle(itemDto.title)
                .withPerson(SecurityService.getLoggedInPerson())
                .withFather(father)
                .withColor(itemDto.color).createItem();

        itemRepository.save(item);
    }

    @RequestMapping(value = "/new", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getAddItemAsHtml() {
        ModelAndView modelAndView = new ModelAndView("createitem");
        modelAndView.addObject("items", itemAssembler.toResources(itemRepository.findByPerson(SecurityService.getLoggedInPerson())));
        return modelAndView;
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedResources<ItemDto> getItemAsJson(@RequestParam(required = false) String search, @PageableDefault(size = 20, sort = "title")Pageable pageable, PagedResourcesAssembler<Item> pagedResourcesAssembler) {
        return pagedResourcesAssembler.toResource(itemRepository.findAll( new ItemSpecification(SecurityService.getLoggedInPerson(), search),pageable), itemAssembler);
    }

    @RequestMapping(value = "/lastused", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ItemDto> getLastUsedItemsAsJson(@RequestParam(required = false) Integer limit) {
        return itemAssembler.toResources(itemService.getLastUsedItems(limit));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ItemDto getItemWithIdAsJson(@PathVariable UUID id) {
        return itemAssembler.toResource(itemRepository.findOne(id));
    }

    @RequestMapping(value = "/{id}/children", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ItemDto> getChildrenOfItemAsJson(@PathVariable UUID id) {
        Item father = itemRepository.getOne(id);
        List<Item> items = itemRepository.findByPersonAndFather(SecurityService.getLoggedInPerson(), father);
        return itemAssembler.toResources(items);
    }

    @RequestMapping(value = "/{id}/haschildren", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Boolean> getItemHasChildrenOfItemAsJson(@PathVariable UUID id) {
        Item father = itemRepository.getOne(id);
        return Collections.singletonMap("children", itemRepository.countByPersonAndFather(SecurityService.getLoggedInPerson(), father) > 0);
    }


    @RequestMapping(value = "/root", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ItemDto> getRootItemsAsJson() {
        List<Item> items = itemRepository.findByPersonAndFather(SecurityService.getLoggedInPerson(), null);
        return itemAssembler.toResources(items);
    }

}
